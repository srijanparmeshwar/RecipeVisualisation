var svg;
var render;
var graphText;

function clearDialog() {
    var title = document.getElementById("dialog-title");
    var ingredients = document.getElementById("dialog-ingredients");
    var instructions = document.getElementById("dialog-instructions");
    var graphContainer = document.getElementById("graph-container");

    title.innerHTML = "";
    ingredients.innerHTML = "";
    instructions.innerHTML = "";
    graphContainer.setAttribute("class", "hide");

    graphText = "";
}

function displayRecipe(xhrResponse) {
    clearDialog();

    var title = document.getElementById("dialog-title");
    var ingredients = document.getElementById("dialog-ingredients");
    var instructions = document.getElementById("dialog-instructions");
    var graphContainer = document.getElementById("graph-container");

    var parts = xhrResponse.split(";;;");
    var response = JSON.parse(parts[0]);

    title.innerHTML = response.title;

    for(var i = 0; i < response.ingredients.length; i++) {
        var ingredientString = response.ingredients[i];
        var ingredient = document.createElement("li");
        ingredient.innerHTML = ingredientString;
        ingredients.appendChild(ingredient);
    }

    for(var i = 0; i < response.instructions.length; i++) {
        var instructionString = response.instructions[i];
        var instruction = document.createElement("li");
        instruction.innerHTML = instructionString;
        instructions.appendChild(instruction);
    }

    graphContainer.setAttribute("class", "");
    graphText = parts[1];
}

function getRecipe(link) {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "../api/recipes/" + link, true);
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            if(xhr.status == 200) {
                displayRecipe(xhr.response);
                renderGraph();
            } else {
                var title = document.getElementById("dialog-title");
                title.innerHTML = "Could not load recipe :(";
                graphText = "";
            }
            dialog.open();
        }
    };
    xhr.send();
}

function processUpload(recipe) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "../api/upload", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4) {
            if(xhr.status == 200) {
                displayRecipe(xhr.response);
                renderGraph();
            } else {
                clearDialog();
                var title = document.getElementById("dialog-title");
                title.innerHTML = "Could not load recipe :(";
                graphText = "";
            }
            dialog.open();
        }
    };
    xhr.send(JSON.stringify(recipe));
}

function paperInput(id, label) {
    var input = document.createElement("paper-input");
    input.setAttribute("id", id);
    input.setAttribute("label", label);
    input.setAttribute("class", "form-input");
    return input;
}

function paperTextArea(id) {
    var input = document.createElement("paper-textarea");
    input.setAttribute("id", id);
    input.setAttribute("rows", "5");
    input.setAttribute("no-label-float", "");
    input.setAttribute("class", "form-textarea");
    return input;
}

function paperButton(id) {
    var button = document.createElement("paper-button");
    button.setAttribute("id", id);
    button.setAttribute("raised", "");
    button.setAttribute("class", "form-button");
    button.addEventListener("click", function() {
        var title = document.getElementById("title-input").value;
        if(!title) title = "";
        var ingredients = document.getElementById("ingredients-input").value;
        if(!ingredients) ingredients = "";
        var instructions = document.getElementById("instructions-input").value;
        if(!instructions) instructions = "";
        var recipe = {
            title: title,
            summary: "",
            ingredients: ingredients.split("\n"),
            instructions: instructions.split("\n"),
            data: ""
        };
        processUpload(recipe)
    });
    button.innerText = "SUBMIT";
    return button;
}

function showForm() {
    clearDialog();
    clearBackground();

    var title = document.getElementById("dialog-title");
    var summary = document.getElementById("dialog-summary");
    var ingredients = document.getElementById("dialog-ingredients");
    var instructions = document.getElementById("dialog-instructions");
    var graphContainer = document.getElementById("graph-container");

    graphContainer.setAttribute("class", "hide");

    title.appendChild(paperInput("title-input", "Title"));
    ingredients.appendChild(paperTextArea("ingredients-input"));
    instructions.appendChild(paperTextArea("instructions-input"));
    instructions.appendChild(paperButton("button-input"));

    dialog.open();
}

function renderGraph() {
    if(graphText.length > 0) {
        var g = graphlibDot.read(graphText);
        d3.select("#graph-svg g").call(render, g);
    }
}

loaders.push(function() {
    svg = d3.select("#graph-svg"),
        inner = d3.select("#graph-svg g"),
        zoom = d3.behavior.zoom().on("zoom", function() {
            inner.attr("transform", "translate(" + d3.event.translate + ")" +
                "scale(" + d3.event.scale + ")");
        });
    svg.call(zoom);
    render = dagreD3.render();
    dialog.addEventListener("iron-overlay-opened", function() {
        renderGraph();
    });
});

