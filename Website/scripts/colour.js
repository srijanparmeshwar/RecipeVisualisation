function Colour(r, g, b, a) {
    this.r = r;
    this.g = g;
    this.b = b;
    if(a) this.a = a;
    else this.a = 1;
}

Colour.prototype = {
    add: function (c) {
        var red = Math.min(255, this.r + c.r);
        var green = Math.min(255, this.g + c.g);
        var blue = Math.min(255, this.b + c.b);
        var alpha = Math.min(1, this.a + c.a);
        return new Colour(red, green, blue, alpha);
    },
    subtract: function (c) {
        var red = Math.max(0, this.r - c.r);
        var green = Math.max(0, this.g - c.g);
        var blue = Math.max(0, this.b - c.b);
        var alpha = Math.max(0, this.a - c.a);
        return new Colour(red, green, blue, alpha);
    },
    multiply: function (value) {
        var red = Math.min(255, Math.round(this.r * value));
        var green = Math.min(255, Math.round(this.g * value));
        var blue = Math.min(255, Math.round(this.b * value));
        var alpha = Math.min(1, Math.round(this.a * value));
        return new Colour(red, green, blue, alpha);
    },
    getRed: function () {
        return this.r;
    },
    getGreen: function () {
        return this.g;
    },
    getBlue: function () {
        return this.b;
    },
    getAlpha: function () {
        return this.a;
    },
    toHSV: function () {
        var M = Math.max(this.r, Math.max(this.g, this.b));
        var m = Math.min(this.r, Math.min(this.g, this.b));
        var C = M - m;
        var Hd = 0;
        if (C === 0) Hd = 0;
        if (M === this.r) Hd = ((this.g - this.b) / C) % 6;
        if (M === this.g) Hd = ((this.b - this.r) / C) + 2;
        if (M === this.b) Hd = ((this.r - this.g) / C) + 4;
        var H = 60 * Hd;
        var S = 0;
        var V = M;
        if (V === 0) S = 0;
        else S = C / V;
        return [H, S, V];
    },
    magnitude: function () {
        return Math.sqrt(this.r * this.r + this.g * this.g + this.b * this.b + this.a * this.a);
    },
    toString: function () {
        return "rgba(" + Math.round(this.r) + ", " + Math.round(this.g) + ", " + Math.round(this.b) + ", " + Math.round(this.a) + ")";
    }
};

Colour.MID = new Colour(122, 122, 122, 0.5);
Colour.RED = new Colour(255, 0, 0, 1);
Colour.GREEN = new Colour(0, 255, 0, 1);
Colour.BLUE = new Colour(0, 0, 255, 1);
Colour.TRANSPARENT = new Colour(0, 0, 0, 0);
Colour.BLACK = new Colour(0, 0, 0, 1);
Colour.WHITE = new Colour(255, 255, 255, 1);

Colour.fromHSV = function (H, S, V, alpha) {
    var C = S * V;
    var Hd = H / 60;
    var X = C * (1 - Math.abs((Hd % 2) - 1));
    var R1 = 0;
    var G1 = 0;
    var B1 = 0;
    if (Hd >= 0 && Hd < 1) {
        R1 = C;
        G1 = X;
        B1 = 0;
    }
    if (Hd >= 1 && Hd < 2) {
        R1 = X;
        G1 = C;
        B1 = 0;
    }
    if (Hd >= 2 && Hd < 3) {
        R1 = 0;
        G1 = C;
        B1 = X;
    }
    if (Hd >= 3 && Hd < 4) {
        R1 = 0;
        G1 = X;
        B1 = C;
    }
    if (Hd >= 4 && Hd < 5) {
        R1 = X;
        G1 = 0;
        B1 = C;
    }
    if (Hd >= 5 && Hd < 6) {
        R1 = C;
        G1 = 0;
        B1 = X;
    }
    var m = V - C;
    var R = R1 + m;
    var G = G1 + m;
    var B = B1 + m;
    return new Colour(R, G, B, alpha);
};
Colour.interpolateRGB = function (A, B, n) {
    if (n === 0) return [];
    if (n === 1) return [A];
    var colours = [];
    var iR = (B.r - A.r) / (n - 1);
    var iG = (B.g - A.g) / (n - 1);
    var iB = (B.b - A.b) / (n - 1);

    var r = A.r;
    var g = A.g;
    var b = A.b;
    for (var i = 0; i < n; i++) {
        colours.push(new Colour(r, g, b, 1));
        r += iR;
        g += iG;
        b += iB;
    }
    return colours;
};
Colour.interpolateHSV = function (A, B, n) {
    if (n === 0) return [];
    if (n === 1) return [A];
    var colours = [];
    var HSVA = A.toHSV();
    var HSVB = B.toHSV();
    var iH = (HSVB[0] - HSVA[0]) / (n - 1);
    var iS = (HSVB[1] - HSVA[1]) / (n - 1);
    var iV = (HSVB[2] - HSVA[2]) / (n - 1);

    var h = HSVA[0];
    var s = HSVA[1];
    var v = HSVA[2];
    for (var i = 0; i < n; i++) {
        colours.push(Colour.fromHSV(h, s, v, 1));
        h += iH;
        s += iS;
        v += iV;
    }
    return colours;
};

Colour.BASE = [new Colour(255, 82, 82, 1), new Colour(33, 150, 243, 1)];

Colour.getPalette = function(count) {
    return Colour.interpolateHSV(Colour.BASE[0], Colour.BASE[1], count);
}

Colour.THRESHOLD = Colour.MID.magnitude();