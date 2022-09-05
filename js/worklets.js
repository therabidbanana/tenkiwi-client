import { useDerivedValue } from 'react-native-reanimated';

// Cribbed from https://github.com/status-im/status-mobile/pull/13470/files#diff-b781e7358db4554f04b8ce6039b73cc8cab11c3daad5490c7bfb22703d93cc6d
// 
// https://github.com/status-im/status-mobile/blob/develop/LICENSE.md
// Generic Worklets
window.workletHack = function applyAnimationsToStyle(animations, style) {
    return function() {
        'worklet'

        var animatedStyle = {}

        for (var key in animations) {
            if (key == "transform") {
                var transforms = animations[key];
                var animatedTransforms = []

                for (var transform of transforms) {
                    var transformKey = Object.keys(transform)[0];
                    animatedTransforms.push({
                        [transformKey]: transform[transformKey].value
                    })
                }

                animatedStyle[key] = animatedTransforms;
            } else {
                animatedStyle[key] = animations[key].value;
            }
        }

        return Object.assign(animatedStyle, style);
    };
};
