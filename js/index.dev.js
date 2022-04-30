import { registerRootComponent } from 'expo';
import {npmDeps} from "../target/expo/npm_deps.js";

var options = {optionsUrl: "http://10.0.0.101:19000/target/expo/cljsc_opts.json"};

var figBridge = require("react-native-figwheel-bridge");
figBridge.shimRequire(npmDeps);
registerRootComponent(figBridge.createBridgeComponent(options));
