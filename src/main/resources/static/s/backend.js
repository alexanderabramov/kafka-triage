export function replay(topic) {
    return fetch(`/topics/${topic.name}/records/0/1000000/replay`, { method: 'POST'})
      .catch(function(ex) {
        console.log(`replaying ${topic.name} failed`, ex)
      })
  }
