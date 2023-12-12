# What is this?
This is just some Java support for the fantastic Ollama project (https://ollama.ai/).

# What can I do with it (Ollama)?
Run AI on your own hardware. Whatever that hardware is. I actually (also) run it on a Raspberry Pi, so, yeah, mostly everything. It will need to have memory (RAM) and unless you have some NVidia card, it will be VERY slow. But it will run.

Why would I want to do that? To learn, foremost. If you have a use-case, to run that without paying the man and, above all, without him looking over your shoulder.

# What is here?

## Chat frame.
A basic chat application is provided to chat with any model with some additional information.
It uses the client mentioned above.
This is the default, just install Ollama, run a model and chat with it. Install another model. Install models on other computers and chat with those.

## Client
The client class provides support for working with modela via the Ollama server or even Ollama servers on other machines. This includes branching, this latter feature is experimental, code level only.

## JSON stuff, for use with Java.
The various POJO's that correspond with the Ollama API documentation. This is only interesting if you code in Java.

## Model / System interaction.
One great aspect of running an LLM locally is that you can permit it to execute programs and
utilities on your behalf.
- SVG support. Most models can output basic SVG images, passing these through ImageMagik allows to display or store the output as PNG. Very usefull to create an icon for instance.
- GraphViz support. By having the model generate a DOT file, you can get a quick (directed) graph of a subject.
- PlantUML support. Most models will be able to output an UML
class diagram from code you described or were working on with the model.
- Vagrant support. Letting the model run arbitrary code on your live system is *NOT* a good idea. However, running a vagrant instance and allowing it to _curl_ it's little hearth out inside of that is fairly safe and super powerful. This is working but unlike ChatGPT, these smaller models get quite confused by this. So it has limited use for now.

_2023-11-20_: SVG, DOT and UML are implemented as I could port those over from
earlier projects. Just set the "auto" checkmark in the chat interface
and tell the model "Output a simple SVG file please". The chat interface
will recognize the SVG in the output and pop up a window with the result.
You can even edit the SVG in place.
That window needs some GUI work and it would be nice to be able to save things,
that is being worked on.

_2023-11-24_: Most things are working now, as mentioned on this page. Still work left though.

_2023-12-11_: Debate! Have two models hash it out against each other (or themselves) and have a model judge the result. Tested with quite amazing results, IMHO:
[Sample run in DebateDemo.java](debateDemo.md)
