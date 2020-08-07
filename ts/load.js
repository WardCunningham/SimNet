
// load traffic into the network based on arrival parameters
// usage: deno run --allow-net ts/load.js > pop-5sec-60min.json

const data = await fetch('http://simnet.ward.asia.wiki.org/assets/pages/next-hop-routing/data.json')
  .then(res => res.json())

const stations = {}; 
  for (let s of data) stations[s.zip] = s

let eventQueue = [];
let totalPop = data.reduce((sum,each) => sum+each.pop, 0)
// console.log({totalPop})

const uniform = max => Math.random() * max
const exponential = (avg) => (- Math.log(uniform(1))) * avg

function anyone() {
  let pick = uniform(totalPop)
  let skipped = 0
  let candidate = 0
  while((skipped += data[candidate].pop) < pick) candidate++
  return data[candidate].zip
}

function anyplace() {
  let pick = Math.floor(uniform(data.length))
  return data[pick].zip
}

let lastArrival = 3600.0
let interArrival = 5.0
let arrivalTime = 2.0

function arrival() {
  let when = clock + exponential(interArrival)
  eventQueue.push(when)
  eventQueue.sort()
}

let clock = 0.0
arrival()
while (clock <= lastArrival) {
  clock = eventQueue.shift()
  let from = anyone()
  let to = anyone()
  console.log(`[${clock.toFixed(3)}, ${from}, ${to}]`)
  arrival()
}