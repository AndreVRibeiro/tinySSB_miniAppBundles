"use strict";

function sketch_reduceResolution(base64String, reductionFactor) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.src = base64String;

        // Wait for the image to load
        img.onload = function () {
          const canvas = document.createElement("canvas");

          const reducedWidth = img.width / reductionFactor;
          const reducedHeight = img.height / reductionFactor;
          canvas.width = reducedWidth;
          canvas.height = reducedHeight;

          const ctx = canvas.getContext("2d");

          ctx.drawImage(img, 0, 0, reducedWidth, reducedHeight);
          const resultBase64String = canvas.toDataURL("image/png");
          resolve(resultBase64String);
        };

        img.onerror = function () {
          reject(new Error("Sketch - Failed to reduce resolution"));
        };
      });
}

// returns the size of the base64 string that represents the sketch
async function sketch_get_current_size() {
    let sketch = await sketch_getImage()
    return sketch.length
}

// return the current sketch as a base64 string (including the preceding data type descriptor)
async function sketch_getImage() {
    let canvas = document.getElementById("sketchCanvas")
    var drawingUrl = canvas.toDataURL('image/png');
    var reductionFactor = Math.max(canvas.width / SKETCH_MAX_WIDTH, canvas.height / SKETCH_MAX_HEIGHT)
    if (reductionFactor > 1) {
        drawingUrl = await sketch_reduceResolution(drawingUrl, reductionFactor)
    }

    var data = drawingUrl.split(',')[1];

    // We Convert the data to a Uint8Array
    var byteArray = atob(data)
      .split('')
      .map(function (char) {
        return char.charCodeAt(0);
      });
    var uint8Array = new Uint8Array(byteArray);

    // We Use pako to compress the Uint8Array
    var compressedData = pako.deflate(uint8Array);

    // We Convert the compressed data back to a base64 string
    var compressedBase64 = btoa(String.fromCharCode.apply(null, compressedData));

    // We Create a new data URL with the compressed data
    var shortenedDataURL = 'data:image/png;base64,' + compressedBase64;


    return shortenedDataURL
}

//function called by the drawing submit button
async function chat_sendDrawing() {
    var sketch = await sketch_getImage()
    if (sketch.length == 0) {
            return;
    }

    // send to backend
    var recps;
    if (curr_chat == "ALL") {
        recps = "ALL";
        backend("publ:post [] " + btoa(sketch) + " null"); //  + recps)
    } else {
        recps = tremola.chats[curr_chat].members.join(' ');
        backend("priv:post [] " + btoa(sketch) + " null " + recps);
    }
    closeOverlay();
    setTimeout(function () { // let image rendering (fetching size) take place before we scroll
        var c = document.getElementById('core');
        c.scrollTop = c.scrollHeight;
    }, 100);

    // close sketch
    chat_closeSketch();
}

// Event Emitter used by load_post_item() to display the sketch in the chat
window.eventEmitter.on('load_post_item', (txt, box) => {
    // Sketch app
    if (txt.startsWith("data:image/png;base64")) { // check if the string is a data url
        var compressedBase64 = txt.split(',')[1];
        // We Convert the compressed data from a base64 string to a Uint8Array
        var compressedData = atob(compressedBase64).split('').map(function (char) {return char.charCodeAt(0);});
        var uint8Array = new Uint8Array(compressedData);
        // We to decompress the Uint8Array
        var decompressedData = pako.inflate(uint8Array);
        // We Convert the decompressed data back to a base64 string
        var decompressedBase64 = btoa(String.fromCharCode.apply(null, decompressedData));
        // We Create a new data URL with the decompressed data
        var decompressedDataURL = 'data:image/png;base64,' + decompressedBase64;
        //display the data url as an image element
        box += "<img src='" + decompressedDataURL + "' alt='Drawing' style='width: 50vw;'>";
        txt = "";
        return { txtChanged: true, boxChanged: true, txt, box };
    }
    return { txtChanged: false, boxChanged: false, txt, box };
});

function miniapp_openSketch() {

    closeOverlay();
    var lst = scenarioDisplay['sketch'];
    display_or_not.forEach(function (d) {
        // console.log(' l+' + d);
        if (lst.indexOf(d) < 0) {
            document.getElementById(d).style.display = 'none';
        } else {
            document.getElementById(d).style.display = null;
            // console.log(' l=' + d);
        }
    })

    document.getElementById('tremolaTitle').style.position = null;

    document.getElementById('tremolaTitle').style.display = null;
    document.getElementById('conversationTitle').style.display = 'none';

    prev_scenario = curr_scenario;
    curr_scenario = 'sketch';
    document.getElementById('core').style.height = 'calc(100% - 118px)';

    launch_snackbar("Select the chat you want to send a sketch to!");
    console.log('curr_scenario: ' + curr_scenario)
    console.log('prev_scenario: ' + prev_scenario)
}