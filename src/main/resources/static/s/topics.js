const e = React.createElement;
const ReactRouter = window.ReactRouterDOM
const Link = ReactRouter.Link

export class TopicList extends React.Component {
  constructor(props) {
    super(props);

    this.state = { fetchInProgress: true, topics: [], replayInProgress: false };

    this.onReplayedAll = this.onReplayedAll.bind(this);
    this.onTopicsFetched = this.onTopicsFetched.bind(this);
    this.replayAll = this.replayAll.bind(this);
  }

  componentDidMount() {
    this.fetchTopics();
  }

  render() {
    return [
      e('table', null, [
        e('thead', null,
            e('tr', null, [e('th', null, 'topic'), e('th', null, 'error lag')])),
        e('tbody', null,
            this.state.topics.map((topic) => e(TopicRow, {key: topic.name, topic: topic})))]),
      e('button', {onClick: this.replayAll, disabled: this.state.fetchInProgress || this.state.replayInProgress}, 'replay all') ]
  }

  fetchTopics() {
    this.setState({fetchInProgress: true});

    fetch('/topics/')
      .then(function(response) {
        return response.json()
      }).then(this.onTopicsFetched)
      .catch(function(ex) {
        console.log('retrieving topics failed', ex)
      })
  }

  onTopicsFetched(json) {
    this.setState({fetchInProgress: false, topics: json})
  }

  replayAll() {
    this.setState({replayInProgress: true})
    const replayPromises = this.state.topics.map(this.replay)
    Promise.all(replayPromises)
      .then(this.onReplayedAll)
  }

  replay(topic) {
    return fetch(`/topics/${topic.name}/records/0/0/replay`, { method: 'POST'})
      .catch(function(ex) {
        console.log(`replaying ${topic.name} failed`, ex)
      })
  }

  onReplayedAll() {
    this.setState({replayInProgress: false})
    this.fetchTopics();
  }
}

export class TopicRow extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const topic = this.props.topic
    return e('tr', null, [
      e('td', null, e(Link, {to: {pathname: `/topics/${topic.name}/records`} }, topic.name)),
      e('td', null, topic.lag)]);
  }
}
