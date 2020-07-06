// convert simnet data to wiki map markup
// usage: deno run --allow-read data.js | pbcopy

import { readJson, readJsonSync }
  from "https://deno.land/std/fs/read_json.ts";

const data = readJsonSync("./data.json");
for (let props of data) {
  let lat = props.details[2]/100
  let lon = - props.details[3]/100
  console.log(`${lat}, ${lon} ${props.city}`)
}