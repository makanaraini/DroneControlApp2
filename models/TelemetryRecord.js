const mongoose = require('mongoose');

const telemetrySchema = new mongoose.Schema({
  speed: {
    type: Number,
    required: true
  },
  battery: {
    type: Number,
    required: true
  },
  altitude: {
    type: Number,
    required: true
  },
  latitude: {
    type: Number,
    required: true
  },
  longitude: {
    type: Number,
    required: true
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
});

// Index by timestamp for faster queries
telemetrySchema.index({ timestamp: 1 });

module.exports = mongoose.model('TelemetryRecord', telemetrySchema); 