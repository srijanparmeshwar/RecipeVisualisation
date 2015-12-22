var cards = [];

function key(event) {
    if (event.which == 13 || event.keyCode == 13) {
        document.getElementById("go").click();
    }
    return true;
}

function clear() {
    for(var i = 0; i<cards.length; i++) {
        cards[i].remove();
    }
    cards = [];
}

function error() {
    clear();
    cards.push(textCard(":(", "No results found."));
    cards[0].attachTo(document.getElementById("cards"));
}

function search(value) {
    var main = document.getElementById("main");
    var hint = document.getElementById("hint");
    if(hint) {
        main.removeChild(document.getElementById("hint"));
        document.body.style.background = "#F44336";
    }

    clear();
    cards.push(progressCard());
    cards[0].attachTo(document.getElementById("cards"));

    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/search?q=" + value, true);
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            if(xhr.status == 200) {
                update(xhr.response);
            } else {
                error();
            }
        }
    };
    xhr.send();
}

function update(response) {
    var results = JSON.parse(response);
    clear();
    for(var i = 0; i<results.length; i++) {
        var result = JSON.parse(results[i]);
        cards.push(linkCard(result.title, result.link));
    }
    for(var i = 0; i<cards.length; i++) {
        cards[i].attachTo(document.getElementById("cards"));
    }
}

function progressCard() {
    var card = new Card(Colour.WHITE);
    card.setProgress();
    card.addRipple();
    return card;
}

function linkCard(title, link) {
    var card = textCard(title, "");
    card.addLink(link);
    return card;
}

function textCard(heading, content) {
    var card = new Card(Colour.WHITE);
    card.setHeading(heading);
    if(content.length>0) card.setContent(content);
    card.addRipple();
    return card;
}

window.onload = function() {
    document.getElementById("go").addEventListener("click", function() {
        search(encodeURIComponent(document.getElementById("query").value));
    });
};
