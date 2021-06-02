
Notes

1. Run lein figwheel first
2. Boot expo with expo start
3. Press w to boot web version 
4. Ignore CORS with https://chrome.google.com/webstore/detail/moesif-origin-cors-change/digfbfaphojjndkpccljibejjbppifbc/related (ew) and only connect with localhost.


Relevant changes to get cross-platform compile were in package.json and babel.config.js

See example of web specific change core.cljs

Libraries:

(MIT) https://github.com/callstack/react-native-paper

(MIT) https://github.com/osdnk/react-native-reanimated-bottom-sheet
 (+ MIT Deps) 
 https://github.com/software-mansion/react-native-reanimated
 https://github.com/software-mansion/react-native-gesture-handler

(MIT) https://github.com/satya164/react-native-tab-view
 (+ MIT Dep)
 https://github.com/callstack/react-native-pager-view

(MIT) https://github.com/iamacup/react-native-markdown-display


To figure out - 

1. Custom bottom-sheet for web that's fixed to bottom (make sure enough padding in under element?)
2. How to host web version at tenkiwi
3. How to extract standard shared elements like the action tray
4. Main bottleneck - reading large amounts of edn (time to stream only necessary stuff down?)

## your-project

### Usage

#### Install Expo [XDE and mobile client](https://docs.expo.io/versions/v15.0.0/introduction/installation.html)
    If you don't want to use XDE (not IDE, it stands for Expo Development Tools), you can use [exp CLI](https://docs.expo.io/versions/v15.0.0/guides/exp-cli.html).

``` shell
    yarn global add exp
```

#### Install [Lein](http://leiningen.org/#install) or [Boot](https://github.com/boot-clj/boot)

#### Install npm modules

``` shell
    yarn install
```

#### Signup using exp CLI

``` shell
    exp signup
```

#### Start the figwheel server and cljs repl

##### leiningen users
``` shell
    lein figwheel
```

##### boot users
``` shell
    boot dev

    ;; then input (cljs-repl) in the connected clojure repl to connect to boot cljs repl
```

#### Start Exponent server (Using `exp`)

##### Also connect to Android device

``` shell
    exp start -a --lan
```

##### Also connect to iOS Simulator

``` shell
    exp start -i --lan
```

### Add new assets or external modules
1. `require` module:

``` clj
    (def cljs-logo (js/require "./assets/images/cljs.png"))
    (def FontAwesome (js/require "@expo/vector-icons/FontAwesome"))
```
2. Reload simulator or device

### Make sure you disable live reload from the Developer Menu, also turn off Hot Module Reload.
Since Figwheel already does those.

### Production build (generates js/externs.js and main.js)

#### leiningen users
``` shell
lein prod-build
```

#### boot users
``` shell
boot prod
```
