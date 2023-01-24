const http = require('http'); 
const fs = require('fs');
const fse = require('fs-extra');
const uuid = require('uuid');
const tempDir = require('temp-dir');
const unzipper = require('unzipper');

const sourceUrl = "http://localhost:8080/ts.zip";
const tmpPath = tempDir + '/' + uuid.v4();

console.log("Downloading " + sourceUrl + "...");
const request = http.get(sourceUrl, function(response) {
    const extractor = unzipper.Extract({path: tmpPath});
    response.pipe(extractor);
    extractor.on("close", function() {
        console.log("Downloaded & unzipped " + sourceUrl + ", the unzipped path is " + tmpPath);
        console.log("Delete src/__generated");
        fs.rmSync("src/__generated", { recursive: true, force: true });
        console.log("Move " + tmpPath + " to src/__generated");
        fse.moveSync(tmpPath, "src/__generated", { overwrite: true });
        console.log("Api code is refreshed sucessfully");
    });
});