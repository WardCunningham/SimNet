// convert simnet data to wiki map pages in export format
// usage: deno run --allow-read zip.js > export.json

import { readJson, readJsonSync }
  from "https://deno.land/std/fs/read_json.ts";

const id = () => Math.floor((Math.random()*2**52)).toString(36)
const asSlug = (title) => title.replace(/\s/g, '-').replace(/[^A-Za-z0-9-]/g, '').toLowerCase();
const desc = [
  'Northeast',
  'Southeast',
  'Midwest',
  'Ozarks',
  'Easter Great Plains',
  'Colorado Plateau',
  'Western Great Plains',
  'California',
  'Northwest'
]
const data = readJsonSync("./data.json");

let tree = []
for (let props of data) {
  let [z,r,s] = props.details[0].map(i=>i-1)
  tree[z] = tree[z] || []
  tree[z][r] = tree[z][r] || []
  tree[z][r][s] = props
}

let exported = {}
for (const [z, zone] of tree.entries()) {
  let title = `Zone ${z+1}`
  let synopsis = `${desc[z]} has ${zone.length} regions with ${zone.map(r=>r.length).join(", ")} stations each.`
  let story = [{type:'paragraph',text:synopsis,id:id()},{type:'paragraph',text:' See [[Topo Map]] of complete zone.',id:id()}]
  for (const [r, region] of zone.entries()) {
    let cities = region.map(props => props.city).join(", ")
    story.push({type:'paragraph',text:`Region ${r+1}: ${cities}`,id:id()})
    let markers = region.map(props => {
      let lat = props.details[2]/100
      let lon = - props.details[3]/100
      return `${lat}, ${lon} ${props.city}`
    }).join("\n")
    story.push({type:'map', text:markers, id:id()})
  }
  let page = exported[asSlug(title)] = {title, story}
  let item = JSON.parse(JSON.stringify(page))
  let date = Date.now()
  let source = 'https://github.com/WardCunningham/SimNet'
  let script = 'zip.js'
  let journal = page['journal'] = [{type:'create', date, source, script},{type:'fork', date}]
}

console.log(JSON.stringify(exported,null,2))
