import * as backend from "./backend.js"

const e = React.createElement;
const ReactRouter = window.ReactRouterDOM
const Link = ReactRouter.Link

export class TopicList extends React.Component {
  constructor(props) {
    super(props);

    this.state = { fetchInProgress: true, topics: [], replayInProgress: false };

    this.onReplayedAll = this.onReplayedAll.bind(this);
    this.onTopicsFetched = this.onTopicsFetched.bind(this);
    this.replay = this.replay.bind(this);
  }

  componentDidMount() {
    this.fetchTopics();
  }

  render() {
    return e('table', {key:'topic-table', class:'pure-table'}, [
        e('thead', {key:'thead'},
            e('tr', null, [e('th', {key:'topic'}, 'topic'), e('th', {key:'lag'}, 'error lag'), e('th', {key:'json'}, 'as json'), e('th', {key:'replay'}, '')])),
        e('tbody', {key:'tbody'},
            this.state.topics.map((topic) => e(TopicRow, {key: topic.name, topic: topic, replay: this.replay, fetchInProgress: this.state.fetchInProgress, replayInProgress: this.state.replayInProgress})))])
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

  replay(topic) {
    this.setState({replayInProgress: true})
    return backend.replay(topic)
        .then(this.onReplayedAll)
  }

  onReplayedAll() {
    this.setState({replayInProgress: false})
    this.fetchTopics();
  }
}

export class TopicRow extends React.Component {
  constructor(props) {
    super(props);

    this.replay = this.replay.bind(this);
  }

  replay(topic) {
    this.props.replay(topic)
  }

  render() {
    const topic = this.props.topic
    return e('tr', null, [
      e('td', {key:'topic'}, e(Link, {to: {pathname: `/topics/${topic.name}/records`} }, topic.name)),
      e('td', {key:'lag'}, topic.lag),
      e('td', {key:'topic-json'}, e('a', {href: `/topics/${topic.name}/records`, target: '_blank' }, 'json')),
      e('td', {key:'replay'}, e('button', {key:'replay-button', onClick: this.replay.bind(this, topic), disabled: this.props.fetchInProgress || this.props.replayInProgress}, 'replay'))]);
  }
}
