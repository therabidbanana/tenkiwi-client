# Tenkiwi Client

This is the client side part of Tenkiwi, which shows the user experience in the mobile apps or online. The bulk of the game rules live in the server component - https://github.com/therabidbanana/tenkiwi

## What is it?

Tenkiwi is a platform for playing storytelling games with friends online - a
Virtual Tabletop (VTT) for a very specific kind of game. 

The gameplay is most directly influenced by For the Queen, and there is a
specific mode for playing Descended from the Queen games.

### Supported Game Modes

1. Descended from the Queen ([For the Queen](http://www.forthequeengame.com/)) -
   seen in
   [https://therabidbanana.itch.io/for-the-captain](https://therabidbanana.itch.io/for-the-captain)
2. [Debrief SRD](https://therabidbanana.itch.io/debrief-srd) - semi-competitive,
   with mini character creation, a spoiler character and three rounds of voting.
   Seen in
   [https://therabidbanana.itch.io/culinary-contest](https://therabidbanana.itch.io/culinary-contest)
3. Opera SRD - players solve mysteries - seen in
   [https://therabidbanana.itch.io/this-mission-is-redacted](https://therabidbanana.itch.io/this-mission-is-redacted)
4. [Wretched and Alone SRD](https://sealedlibrary.itch.io/wretched-alone-srd) -
   solo, prompt driven games with unclear numbers of turns and unlikekly win
   conditions - seen in
   [https://therabidbanana.itch.io/outsider-blues](https://therabidbanana.itch.io/outsider-blues)
   
Also some prototype work not ready for primetime.
   
### Game Development

Use the unlock code "custom" to unlock the above games with customizable spreadsheets. Note that there are currently not validations on the file to ensure you've formatted it correctly. Try starting from a template (all of my games directly share the prompt spreadsheets as a google sheet you can clone)

Game templates are TSV files available on the web - you can configure Google Sheets to output a TSV using the "Publish to Web" feature: https://gist.github.com/therabidbanana/c0145b650a5e02ab2ae79e778f423e13

### Server Required

The client only contains the necessary logic to display and interact with the
game. Rules are built into the server and the client is intended to be as dumb
as possible (making it easy to add new games without deploying to the app
store).

### Built with Expo

### And Clojurescript

Communication with the server is supported with Sente. 



## Development

### Usage

#### Install Expo 

Go install the expo-cli: https://docs.expo.dev/get-started/installation

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

### Production build 

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
eas update
```

Web build:

(clojurescript output creates wild warnings)

``` 
npx expo export:web 2>/dev/null
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
eas build --auto-submit
```

Then go to the app store to set it up. (If you're looking for them, builds are in a nonsensical place)

https://appstoreconnect.apple.com/apps/1571524662/testflight/ios

Release requires setting up a new version with the configured build after getting the test flight version out.

#### Android 

```
eas build --auto-submit
```

No other changes required


## Credits

Copyright © 2021 David Haslem

### X-Card

This application adapts the X-Card, originally by John Stavropoulos

[http://tinyurl.com/x-card-rpg](http://tinyurl.com/x-card-rpg).

### Descended from the Queen

Some games in this work are based on [For the Queen](http://www.forthequeengame.com/)
, product of Alex Roberts and Evil Hat Productions, and licensed for our use under the 
[Creative Commons Attribution 3.0 Unported license](http://creativecommons.org/licenses/by/3.0/).

### Redacted Materials

Some games in this work use material from the External Containment Bureau
roleplaying game (found at
[https://mythicgazetteer.itch.io/external-containment-bureau](https://mythicgazetteer.itch.io/external-containment-bureau)),
designed by Lexi Antoku, Eric Brunsell, Michael Elliott, Justin Ford, and Eli
Kurtz, and published by Mythic Gazetteer LLC, pursuant to the open license
available at
[mythicgazetteer.com/redacted-material](http://mythicgazetteer.com/redacted-material/)
