import * as topics from "./topics.js"
import { RecordList } from "./records.js"

const e = React.createElement;
const ReactRouter = window.ReactRouterDOM
const HashRouter = ReactRouter.HashRouter
const Redirect = ReactRouter.Redirect
const Route = ReactRouter.Route
const Switch = ReactRouter.Switch

class App extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return e(Switch, null,
        e(Route, {path: '/topics/:topic/records', component: RecordList}),
        e(Redirect, {from: '/topics/:topic', to: '/topics/:topic/records'}),
        e(Route, {path: '/topics', component: topics.TopicList}),
        e(Redirect, {from: '', to: '/topics'})
    )
  }
}

const app = e(HashRouter, null,
  e(App)
)

ReactDOM.render(app, document.querySelector('#app'));
