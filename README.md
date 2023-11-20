# Ollama
Java support for the fantastic Ollama project.

## JSON stuff.
This project defines the various POJO's that correspond with the Ollama API documentation.

## Client
The client class provides support for working with a model via the Ollama server. This includes branching.

## Chat frame.
A basic chat application is provided to chat with any model with some additional information.
It uses the client mentioned above.

## Model / System interaction (WIP).
One great aspect of running an LLM locally is that you can permit it to execute programs and
utilities on your behalf. Some of this is Work In Progress, intended functionality will be:
- Get the status of your GPU, like VRAM in use (nvidia-smi).
- _implemented_: SVG support. Most models can output basic SVG images, passing these through ImageMagik allows to display or store the output as PNG. Very usefull to create an icon for instance.
- _implemented_: GraphViz support. By having the model generate a DOT file, you can get a quick (directed) graph of a subject.
- _implemented_: PlantUML support. Most models will be able to output an UML
class diagram from code you described or were working on with the model.
- Vagrant support. Letting the model run arbitrary code on your live system is *NOT* a good idea. However, running a vagrant instance and allowing it to _curl_ it's little hearth out inside of that is fairly safe and super powerful.

_2023-11-20_: SVG, DOT and UML are implemented as I could port those over from
earlier projects. Just set the "auto" checkmark in the chat interface
and tell the model "Output a simple SVG file please". The chat interface
will recognize the SVG in the output and pop up a window with the result.
You can even edit the SVG in place.
That window needs some GUI work and it would be nice to be able to save things,
that is being worked on.

