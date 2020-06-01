import React from 'react';

class JsonDump extends React.Component {
    static propTypes = {
        children: React.PropTypes.any
    }

    render() {
        return <pre>{JSON.stringify(this.props.children, null, 4)}</pre>
    }
}
