module.exports = ({config}) => {
    if (process.env.DEV === "true") {
        let newConfig = {
            ...config,
            entryPoint: "js/index.dev.js"
        };
        // console.log("DEV MODE true...");
        // console.log(config.name);
        // console.log("entryPoint is " + config.entryPoint);
        return newConfig;
    } else {
        return config;
    }
};
