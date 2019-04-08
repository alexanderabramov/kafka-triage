const e = React.createElement;
const ReactVirtualized = window.ReactVirtualized;
const { AutoSizer, CellMeasurer, CellMeasurerCache, Column, Table } = ReactVirtualized;
const ReactJson = window.reactJsonView.default

export class RecordList extends React.Component {
  constructor(props) {
    super(props);

    this.topic = this.props.match.params.topic;
    this.state = { records: [] };
    this.onRecordsRetrieved = this.onRecordsRetrieved.bind(this);

    this._cache = new CellMeasurerCache({
        fixedWidth: true,
        minHeight: 30,
    });

    this._lastRenderedWidth = this.props.width;
  }

  componentDidMount() {
    fetch(`/topics/${this.topic}/records`)
      .then(function(response) {
        return response.json()
      }).then(this.onRecordsRetrieved)
      .catch(function(ex) {
        console.log('retrieving records failed', ex)
      })
  }

  render() {
    const {width} = this.props;
    const records = this.state.records;

    if (this._lastRenderedWidth !== width) {
      this._lastRenderedWidth = width;
      this._cache.clearAll();
    }

    return e(AutoSizer, null, ({ height, width }) =>
      e(Table, {
        headerHeight: 20,
        height: height,
        rowCount: records.length,
        rowGetter: ( {index} ) => records[index],
        rowHeight: this._cache.rowHeight,
        width: width
      },
        e(Column, {
            dataKey: 'partition',
            label: 'Partition',
            width: 100
          }),
        e(Column, {
            dataKey: 'offset',
            label: 'Offset',
            width: 80
          }),
        e(Column, {
            dataKey: 'key',
            label: 'Key',
            width: 100
          }),
        e(Column, {
            dataKey: 'value',
            label: 'Value',
            width: 400,
            flexGrow: 1
          }),
        e(Column, {
            cellRenderer: ({cellData, dataKey, parent, rowIndex}) => e(CellMeasurer,
                {cache: this._cache, columnIndex: 0, key: dataKey, parent: parent, rowIndex:rowIndex},
                e(ReactJson, {collapsed: false, displayDataTypes: false, src: cellData})),
            dataKey: 'headers',
            label: 'Headers',
            width: 400,
            flexGrow: 1
          })
      )
    )
  }

  onRecordsRetrieved(json) {
    this.setState({records: json})
  }
}

export class Record extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const record = this.props.record
    return record.topic + '-' + record.partition + ' ' + record.offset;
  }
}
