function Card(colour) {
    this.element = document.createElement("paper-card");
    this.element.style.background = colour.toString();
    this.element.style.fontFamily = "Lato";
    this.element.setAttribute("elevation", "2");
    this.element.style.color = Colour.BLACK.toString();
    this.element.setAttribute("class", "center white");
}

Card.prototype = {
    setHeading: function(heading) {
        this.element.setAttribute("heading", heading);
    },
    setContent: function(content) {
        this.content = document.createElement("div");
        this.content.innerHTML = content;
        this.content.setAttribute("class", "card-content");
        this.element.appendChild(this.content);
    },
    setProgress: function() {
        this.content = document.createElement("div");
        this.content.setAttribute("class", "card-content");
        this.element.appendChild(this.content);

        this.progress = document.createElement("paper-progress");
        this.progress.setAttribute("indeterminate", "true");
        this.content.appendChild(this.progress);
    },
    addEventListener: function(type, onEvent) {
        this.element.addEventListener(type, onEvent);
    },
    addRipple: function() {
        this.element.appendChild(document.createElement("paper-ripple"));
    },
    attachTo: function(container) {
        this.container = container;
        this.container.appendChild(this.element);
    },
    remove: function() {
        this.container.removeChild(this.element);
    }
};