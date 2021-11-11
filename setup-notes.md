
Random Notes from getting project built

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


#### Start Exponent server (Using `expo`)


``` shell
    yarn start 
```

This will run "DEV=true expo start", using figwheel entry point.

Note that by default live reload will be enabled, which is bad. Turn the "production mode" toggle on to disable that while still having access to perf monitor.


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

Deploying web build to itch.io:

```
butler push web-build therabidbanana/tenkiwi:web
```

(From tenkiwi app:)

```
rm -rf resources/public/static
cp -R ../tenkiwi-client/web-build/* resources/public/
git add resources
git commit -m "Update web build"
git push # Does a heroku release
```



#### ios Doing a native release

Takes 20-30 minutes to generate an ipa file you can download:

```
expo build:ios -t archive
```

(to update permissions)

https://developer.apple.com/account/resources/identifiers/list

```
expo build:ios --clear-provisioning-profile
```

Successfully created Provisioning Profile

  Experience: @therabidbanana/tenkiwi, bundle identifier: com.davidhaslem.tenkiwi
    Provisioning profile (ID: BD4M2L5GT2)
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)


Project Credential Configuration:
  Experience: @therabidbanana/tenkiwi, bundle identifier: com.davidhaslem.tenkiwi
    Provisioning profile (ID: BD4M2L5GT2)
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)

  Distribution Certificate - Certificate ID: 22GJK6K536
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)
  Push Notifications Key - Key ID: KP5JWWK4JK
    Apple Team ID: 3PZEM7YXDE,  Apple Team Name: David Haslem (Individual)

Open Transporter app and put the ipa into it.

Then go to the app store to set it up. (If you're looking for them, builds are in a nonsensical place)

https://appstoreconnect.apple.com/apps/1571524662/testflight/ios

#### Android 

```
expo build:android -t app-bundle
```

Upload into app store:

https://play.google.com/console/u/0/developers/5401255112018197410/app/4973781934088736512/tracks/production?tab=releaseDashboard



### What?

Sometimes running lein prod-build exits with no message:

davidhaslem at Davids-MacBook-Air.local in [~/projects/cljs-apps/tenkiwi-client]  on git:master ✗  7d8ce2e "Support butler release"
20:56:19 › lein prod-build
Compiling ClojureScript...

(dead)

I don't understand this error yet. Retrying enough times seems to have helped. (I also touched a file: env/prod/user.clj to add a useless println)
