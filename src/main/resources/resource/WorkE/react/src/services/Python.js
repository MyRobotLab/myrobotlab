import React, { Component } from "react";

// import brace from "brace";
import AceEditor from "react-ace";
// import { split as SplitEditor } from "react-ace";

// import "brace/mode/java";
import "brace/mode/python";

// import "brace/theme/github";
import "brace/theme/monokai";

import "brace/ext/language_tools";

import Tabs from "react-bootstrap/Tabs";
import Tab from "react-bootstrap/Tab";


export default class Python extends Component {
  state = {
    key: 0
  };

  onChange(newValue) {
    console.log("change", newValue);
  }

  render() {
    return (
      <div>
        <Tabs
          id="controlled-tab-example"
          activeKey={this.state.key}
          onSelect={key => this.setState({ key })}
        >
          <Tab eventKey="runtime" title="runtime.py">
            <AceEditor
            mode="python"
            width="100%"
            theme="monokai"
            onChange={this.onChange}
            name="example"
            fontSize={14}
            showPrintMargin={true}
            showGutter={true}
            highlightActiveLine={true}
            value={`runtime = Runtime.create('runtime','Runtime')`}
            enableBasicAutocompletion={true}
            enableLiveAutocompletion={true}
            editorProps={{ $blockScrolling: true }}
          />
          </Tab>
        </Tabs>

        <div className="Python.Editor">
          
        </div>
      </div>
    );
  }
}
