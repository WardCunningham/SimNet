// interpolate hops connecting start to finish
// deno run --allow-net ts/hops.js > hops-pop-5sec-60min.json


const traffic = await fetch ('http://simnet.ward.asia.wiki.org/assets/pages/message-traffic/pop-5sec-60min.json')
  .then(res => res.text())

const data = await fetch('http://simnet.ward.asia.wiki.org/assets/pages/next-hop-routing/data.json')
  .then(res => res.json())

const stations = {}; 
  for (let s of data) stations[s.zip] = s


const digit = (zip, col) => 
  Math.floor(zip/(10**(2-col)))%10
    // for (let r of [0,1,2]) console.log(r,digit(321,r))

const hop = (from, toward) => {
  for (let row of [0,1,2]) {
    if(digit(from,row) != digit(toward,row)) {
      let col = digit(toward,row)-1
      return stations[from].route[row][col]
    }
  }
  return null
}

const interpolate = (json) => {
  let [clock, here, there] = JSON.parse(json)
  let hops = []
  while (here) {
    hops.push(here)
    here = hop(here, there)
  }
  console.log(JSON.stringify([clock, ...hops]))
}

for (let json of traffic.split(/\r?\n/)) {
  if(json.length) interpolate(json)
}
