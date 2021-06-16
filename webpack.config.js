const createExpoWebpackConfigAsync = require('@expo/webpack-config');

module.exports = async function (env, argv) {
  const config = await createExpoWebpackConfigAsync(env, argv);
  // Or prevent minimizing the bundle when you build.
  if (config.mode === 'production') {
      config.optimization.minimize = false;
      config.devtool = false;
  }
  return config;
};
