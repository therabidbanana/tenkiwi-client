import { registerRootComponent } from 'expo';
import {npmDeps} from "../target/expo/npm_deps.js";

window.assetLibrary = {
    "loading": require("../assets/icons/loading.png"),
    "logo": require("../assets/icons/app.png"),
    "face": require("../assets/images/face.png"),
    "welcome": require("../assets/images/welcome.png"),
};

var options = {optionsUrl: "http://10.0.0.94:19000/target/expo/cljsc_opts.json"};

var figBridge = require("react-native-figwheel-bridge");
figBridge.shimRequire(npmDeps);
registerRootComponent(figBridge.createBridgeComponent(options));
