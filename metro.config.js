const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// No Router
config.resolver.resolverMainFields = ['browser', 'react-native', 'main'];

module.exports = config;