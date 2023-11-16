# Ollama
Java support for the fantastic Ollama project.

## JSON stuff.
This project defines the various POJO's that correspond with the Ollama API documentation.

## Chat frame.
A basic chat application is provided to chat with any model with some additional information.

## Model / System interaction (WIP).
One great aspect of running an LLM locally is that you can permit it to execute programs and
utilities on your behalf. This is Work In Progress, intended functionality will be:
- Get the status of your GPU, like VRAM in use (nvidia-smi).
- SVG support. Most models can output basic SVG images, passing these through ImageMagik allows to display or store the output as PNG. Very usefull to create an icon for instance.
- GraphViz support. By having the model generate a DOT file, you can get a quick (directed) graph of a subject.
- Vagrant support. Letting the model run arbitrary code on your live system is *NOT* a good idea. However, running a vagrant instance and allowing it to _curl_ it's little hearth out inside of that is fairly safe and super powerful.
