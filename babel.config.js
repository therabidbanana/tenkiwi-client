module.exports = {
    presets: ['babel-preset-expo'],
    plugins: ['react-native-paper/babel', 'react-native-reanimated/plugin'],
    env: {
        production: {
            plugins: ['react-native-paper/babel', 'react-native-reanimated/plugin'],
        },
    },
};
