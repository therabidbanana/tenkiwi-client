
Notes

1. Run lein figwheel first (or use M-x cider jack in cljs -> lein -> wait 5 seconds -> figwheel-main -> dev -> wait 60 seconds)
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

1. integrate markdown-display and paper? Rules for rendering to use heading / subheading?
2. j
3. How to extract standard shared elements like the action tray better

## your-project

### Usage

#### Install Expo [XDE and mobile client](https://docs.expo.io/versions/v15.0.0/introduction/installation.html)

Go install the expo-cli: https://blog.expo.io/expo-cli-2-0-released-a7a9c250e99c

```shell
npm install -g expo-cli
```

#### Install [Lein](http://leiningen.org/#install) or [Boot](https://github.com/boot-clj/boot)

#### Install npm modules

``` shell
    yarn install
```

#### Signup using exp CLI

``` shell
    expo register
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

#### Start Exponent server (Using `expo`)

##### Also connect to Android device

``` shell
    expo start
    (hit a)
```

##### Also connect to iOS Simulator

``` shell
    exp start
    (hit i)
```

### Add new assets or external modules
1. `require` module:

``` clj
    (def cljs-logo (js/require "./assets/images/cljs.png"))
    (def FontAwesome (js/require "@expo/vector-icons/FontAwesome"))
```
2. Reload simulator or device



### Figwheel main conversion

Getting prod builds working required updating clojurescript, requiring an update to figwheel-main

Figwheel-main example came from https://github.com/bhauman/react-native-figwheel-bridge, which replaced most of the figwheel-bridge.js and all of env/dev/* in the process. We may want to adapt some of that work.

TODO: 

Currently we have to switch main.js to js/index.js to do a prod build in package.json. How can we require figwheel bridge only in dev mode?

### Make sure you disable live reload from the Developer Menu, also turn off Hot Module Reload.
Since Figwheel already does those.

### Production build (generates js/externs.js and main.js)

#### JS Releases
``` shell
lein clean
lein prod-build
```

Testing build
```
vim package.json # Edit index.dev.js to index.prod.js 
expo start --no-dev
```

Releasing build to Expo

```
expo publish
```

Web build:

``` 
expo build:web
```

Testing web build directly:

```
npx serve web-build
```

(From tenkiwi app:)

```
rm -rf resources/public/static
cp -R ../tenkiwi-client/web-build/* resources/public/
git add resources
git commit -m "Update web build"
git push # Does a heroku release
```

#### Doing a native release

Takes 20-30 minutes to generate an ipa file you can download:

```
expo build:ios -t archive
```

Open Transporter app and put the ipa into it.



Successfully created Provisioning Profile

  Experience: @therabidbanana/tenkiwi, bundle identifier: com.davidhaslem.tenkiwi
    Provisioning profile (ID: QVC3H6C2KU)
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)


Project Credential Configuration:
  Experience: @therabidbanana/tenkiwi, bundle identifier: com.davidhaslem.tenkiwi
    Provisioning profile (ID: QVC3H6C2KU)
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)

  Distribution Certificate - Certificate ID: 22GJK6K536
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)
  Push Notifications Key - Key ID: KP5JWWK4JK
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)
