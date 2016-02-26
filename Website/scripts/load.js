var loaders = [];

window.onload = function() {
    for(var i = 0; i < loaders.length; i++) {
        loaders[i]();
    }
};
