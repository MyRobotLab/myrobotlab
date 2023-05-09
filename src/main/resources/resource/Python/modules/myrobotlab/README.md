# MyRobotLab Python Library

The MyRobotLab Python library provides a simple way to connect to and control a MyRobotLab service. MyRobotLab is an open-source robotics framework that allows you to create and control robots using a variety of languages and platforms.

## Installation
Already installed when myrobotlab services are installed

## Usage

To use the MyRobotLab Python library, you'll first need to connect to a MyRobotLab service. You can do this using the `connect` function:

```python
import myrobotlab

# default will be 
myrobotlab.connect()

# or if connecting to a service running on a different host
myrobotlab.connect('raspi', 25333)
```

## Contributing
Contributions to the MyRobotLab Python library are always welcome. To contribute, simply fork the repository, make your changes, and submit a pull request.

## License
The MyRobotLab Python library is licensed under the MIT License.