// convert simnet data to wiki map markup
// usage: deno run --allow-read data.js | pbcopy

import { readJson, readJsonSync }
  from "https://deno.land/std/fs/read_json.ts";


const data = readJsonSync("./data.json");
const id2zip = (n) => n ? data[n-1].details[0].join('')*1 : 0
const nz = (n) => n!=0


function network (doit) {
  for (let props of data) {
    let zip = id2zip(props.node)
    let city = props.city
    let pop = props.details[1]*1000
    let lat = props.details[2]/100
    let lon = - props.details[3]/100
    let path = props.path.filter(nz).map(id2zip)
    let route = props.route.map(l => l.filter(nz).map (id2zip))
    doit({props,city,zip,pop,lat,lon,path,route})
  }
 }


// network(p => console.log(`${p.lat}, ${p.lon} ${p.props.city}`)


// console.log('strict graph { node [style=filled fillcolor=bisque]')
// network(p => {
//   let color = `${p.props.details[0][1]/9.5} 0.5 1.0`
//   console.log(`"${p.zip}" [fillcolor="${color}" tooltip="${p.props.city}"] `)
//   for (let d of p.path) console.log(`"${p.zip}" -- "${d}"`)
// })
// console.log('}')

let data2 = []
network(p => {
  delete p.props
  data2.push(p)
})
console.log(JSON.stringify(data2,null,2)
  .replace(/(,|\[)[\r\n\s]*(\d)/g,"$1$2")
  .replace(/(\d)[\r\n\s]*\]/g,"$1]"))