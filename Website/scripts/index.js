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

function error(message) {
    clear();
    cards.push(textCard(":(", message));
    cards[0].attachTo(document.getElementById("cards"));
}

function clearBackground() {
    var hint = document.getElementById("hint");
    if(hint) {
        document.getElementById("main").removeChild(document.getElementById("hint"));
        document.body.style.background = "#F44336";
    }
}

function search(value) {
    clearBackground();

    clear();
    cards.push(progressCard());
    cards[0].attachTo(document.getElementById("cards"));

    var xhr = new XMLHttpRequest();
    xhr.open("GET", "../api/search?q=" + value, true);
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            if(xhr.status == 200) {
                update(xhr.response);
            } else {
                error("No results found.");
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

function displayRecipe(link, xhrResponse) {
    var canvasHolder = document.getElementById("canvas-holder");
    var title = document.getElementById("dialog-title");
    var ingredients = document.getElementById("dialog-ingredients");
    var instructions = document.getElementById("dialog-instructions");

    var parts = xhrResponse.split(";;;");
    var response = JSON.parse(parts[0]);

    title.innerHTML = "";
    ingredients.innerHTML = "";
    instructions.innerHTML = "";

    canvasHolder.innerHTML = parts[1];
    title.innerHTML = response.title;

    for(var i = 0; i<response.ingredients.length; i++) {
        var ingredientString = response.ingredients[i];
        var ingredient = document.createElement("li");
        ingredient.innerHTML = ingredientString;
        ingredients.appendChild(ingredient);
    }

    for(var i = 0; i<response.instructions.length; i++) {
        var instructionString = response.instructions[i];
        var instruction = document.createElement("li");
        instruction.innerHTML = instructionString;
        instructions.appendChild(instruction);
    }

    var svg = canvasHolder.firstElementChild;
    if(svg) {
        svg.setAttribute("width", 3*window.innerWidth/5 + "px");
    }
}

function getRecipe(link) {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "../api/recipes/" + link, true);
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            if(xhr.status == 200) {
                displayRecipe(link, xhr.response);
            } else {
                error("Could not load recipe.")
            }
            dialog.open();
        }
    };
    xhr.send();
}

function linkCard(title, link) {
    var card = textCard(title, "");
    card.addEventListener("click", function() {
        getRecipe(link);
    });
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
