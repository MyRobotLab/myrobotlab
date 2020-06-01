// TODO
// https://github.com/teambit/bit

// import React from "react";
import React, { Component } from "react";
// import logo from "./logo.svg";
import "./App.css";

import ReactSearchBox from "react-search-box";
import Loadable from 'react-loadable';


// stylesheet and globals ?  are required for this
import Tabs from "react-bootstrap/Tabs";
import Tab from "react-bootstrap/Tab";

import loadable from '@loadable/component'

const AsyncPage = loadable(props => import(`./services/${props.page}`))

function Widget() {
  return <h1>Widget Says Hello</h1>;
}

function onChange(newValue) {
  console.log("change", newValue);
}

const LoadableService = Loadable({
  loader: () => import('./services/Servo'),
  loading() {
    return <div>Loading...</div>
  }
});

export default class App extends Component {
  constructor(props) {
    super(props);

    this.fetchData();
    // this.fetchFile();
    
    // Don't call this.setState() here!
    this.state = {
      page: "Servo",
      key: 0,
      services: [
        {
          name: "runtime",
          serviceType: "Runtime"
        },
        {
          name: "python",
          serviceType: "Python"
        },
        {
          name: "servo",
          serviceType: "Servo"
        },
        {
          name: "hobbyservo",
          serviceType: "HobbyServo"
        }
      ]
    };
    // this.handleClick = this.handleClick.bind(this);
  }

  fetchData(){
    fetch("https://randomuser.me/api")
    .then(response => response.json())
    .then(parsedJSON => console.log(parsedJSON.results))
    .catch(error => console.log('parse json failed', error))
  }

  fetchFile(){
    fetch("test2.txt").then(res => {
      console.log("res.ok " + res.ok + " " + res.status + " results " + res.results);
      if(res.ok) {
        return res;
      } else {
        throw Error(`Request rejected with status ${res.status}`);
      }
    })
    .catch(console.error)
  }

  render() {
    /*
    console.log(
      this.props.services.map((value, index, key) => {
        return index + " " + key + "-" + value;
      })
    );
    */
    /*
    console.log(this.services.map((value, index, key) => {
      return <li id={index}>{value.name}{key}</li>;
    }))
*/
    return (
      /**
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React 
          
        </a>
      </header>
       {this.loadRemoteComponent("Python.js")}
      */
      <div className="App">
     

        <ReactSearchBox
          placeholder="Placeholder"
          value="Doe"
          data={this.data}
          callback={record => console.log(record)}
        />

       

        <Tabs
          id="controlled-tab-example"
          activeKey={this.state.key}
          onSelect={key => this.setState({ key })}
        >
          {this.state.services.map(service => {
            return (
              <Tab
                eventKey={service.name}
                key={service.name}
                title={service.name}
              >
                {service.name}
                <AsyncPage page={service.serviceType} />
              </Tab>
            );
          })}
        </Tabs>
      </div>
    );
  }
}
