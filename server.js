const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const bodyParser = require('body-parser');
const mongoose = require('mongoose');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// Import models
const TelemetryRecord = require('./models/TelemetryRecord');

const app = express();
const server = http.createServer(app);
const port = process.env.PORT || 3000;
const wsPort = process.env.WS_PORT || 8080;

// Database connection
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/drone-telemetry')
  .then(() => {
    console.log('Connected to MongoDB');
  })
  .catch(err => {
    console.error('Failed to connect to MongoDB', err);
  });

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Store the latest telemetry data
let telemetryData = {
  speed: 0.0,
  battery: 0,
  altitude: 0.0,
  latitude: 0.0,
  longitude: 0.0,
  timestamp: Date.now()
};

// WebSocket connection handler
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws) => {
  console.log('Client connected');
  
  // Send current telemetry data to new client
  ws.send(JSON.stringify(telemetryData));
  
  ws.on('close', () => {
    console.log('Client disconnected');
  });
});

// Broadcast telemetry data to all connected clients
function broadcastTelemetry() {
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(telemetryData));
    }
  });
}

// REST API endpoints
app.post('/telemetry', (req, res) => {
  telemetryData = {
    ...req.body,
    timestamp: Date.now()
  };
  console.log('Received telemetry:', telemetryData);
  
  // Broadcast updated telemetry to all clients
  broadcastTelemetry();
  
  res.status(200).json({ status: 'success' });
});

app.post('/control', (req, res) => {
  const command = req.body.command;
  console.log('Received control command:', command);
  
  // Here you would implement logic to handle the control command
  // This endpoint should only accept requests from the Android app
  
  res.status(200).json({ status: 'success', message: `Command ${command} processed` });
});

// New endpoint for historical data retrieval
app.get('/history', async (req, res) => {
  try {
    // Get query parameters
    const { start, end, limit } = req.query;
    
    // Build query
    const query = {};
    if (start || end) {
      query.timestamp = {};
      if (start) query.timestamp.$gte = new Date(start);
      if (end) query.timestamp.$lte = new Date(end);
    }
    
    // Set limit with default
    const queryLimit = limit ? parseInt(limit) : 1000;
    
    // Fetch data from database
    const historyData = await TelemetryRecord.find(query)
      .sort({ timestamp: -1 })
      .limit(queryLimit);
    
    res.json(historyData);
  } catch (error) {
    console.error("Error fetching history data:", error);
    res.status(500).json({ error: "Failed to fetch history data" });
  }
});

// Get data grouped by time interval (for charts)
app.get('/history/aggregate', async (req, res) => {
  try {
    const { interval, start, end } = req.query;
    
    // Default to last 24 hours if no dates provided
    const startDate = start ? new Date(start) : new Date(Date.now() - 24 * 60 * 60 * 1000);
    const endDate = end ? new Date(end) : new Date();
    
    // Determine time grouping format based on interval
    let timeFormat;
    switch(interval) {
      case 'hour':
        timeFormat = { $dateToString: { format: "%Y-%m-%d %H:00", date: "$timestamp" } };
        break;
      case 'minute':
        timeFormat = { $dateToString: { format: "%Y-%m-%d %H:%M", date: "$timestamp" } };
        break;
      case 'day':
      default:
        timeFormat = { $dateToString: { format: "%Y-%m-%d", date: "$timestamp" } };
    }
    
    const aggregatedData = await TelemetryRecord.aggregate([
      {
        $match: {
          timestamp: { $gte: startDate, $lte: endDate }
        }
      },
      {
        $group: {
          _id: timeFormat,
          avgSpeed: { $avg: "$speed" },
          avgAltitude: { $avg: "$altitude" },
          avgBattery: { $avg: "$battery" },
          minBattery: { $min: "$battery" },
          maxAltitude: { $max: "$altitude" },
          maxSpeed: { $max: "$speed" },
          count: { $sum: 1 }
        }
      },
      {
        $sort: { _id: 1 }
      }
    ]);
    
    res.json(aggregatedData);
  } catch (error) {
    console.error("Error aggregating history data:", error);
    res.status(500).json({ error: "Failed to aggregate history data" });
  }
});

// Serve static web app files
app.use(express.static('public'));

// Start the server
server.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

console.log(`WebSocket server running at ws://localhost:${wsPort}`); 