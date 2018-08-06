const express = require('express');
const multer = require('multer');
const upload = multer({dest: '/uploads/'});
const MongoClient = require('mongodb').MongoClient;
const ObjectID = require('mongodb').ObjectID;
const createError = require('http-errors');
const path = require('path');
const cookieParser = require('cookie-parser');
const logger = require('morgan');
const debug = require('debug')('server:server');
const http = require('http');
const crypto = require('crypto');
const mime = require('mime');
const fs = require('fs');

let db;

function formatMillis(millisec) {
	let seconds = (parseInt(millisec) / 1000).toFixed(0);
	let minutes = Math.floor(seconds / 60);
	let hours = "";
	if (minutes > 59) {
		hours = Math.floor(minutes / 60);
		hours = (hours >= 10) ? hours : "0" + hours;
		minutes = minutes - (hours * 60);
		minutes = (minutes >= 10) ? minutes : "0" + minutes;
	}

	seconds = Math.floor(seconds % 60);
	seconds = (seconds >= 10) ? seconds : "0" + seconds;
	if (hours !== "") {
		return hours + ":" + minutes + ":" + seconds;
	}
	return minutes + ":" + seconds;
}


const app = express();
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'pug');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({extended: false}));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'www')));

app.get('/', (req, res) => {
	res.render('index');
});

app.get('/videos', (req, res) => {

});

app.get('/videos/upload', (req, res) => {
	db.collection('cookingStations').findOne()
		.then((result) => {
			console.log(result);
			let cs = result;
			if (cs == null) {
				cs = {
					name: 'Cooking Station 1',
					cameras: [{name: 'Camera 1'}]
				}
			}
			res.render('upload', {cs: cs});
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/videos/upload/:cs', (req, res) => {
	db.collection('cookingStations').findOne({'_id': ObjectID(req.params.cs)})
		.then((result) => {
			res.render('upload', {cs: result});
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/videos/:id', (req, res) => {
	res.sendFile(req.params.id, {
		root: '/app/videos/',
	});
});

app.get('/api/videos/:id', (req, res) => {
	db.collection('videos').findOne({'_id': ObjectID(req.params.id)})
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/api/videos', (req, res) => {
	db.collection('videos').find().toArray()
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

const videoUpload = upload.fields([{name: 'video', maxCount: 1}, {name: 'timings', maxCount: 1}]);
app.post('/api/videos', videoUpload, (req, res) => {
	if (!req.files) return res.status(400).send('No files were uploaded.');
	if (!req.body.cs) return res.status(400).send('No cooking station selected');
	if (!req.body.camera) return res.status(400).send('No camera selected');
	if (!req.files['video'][0]) return res.status(400).send('No video was uploaded.');

	db.collection('cookingStations').findOne({'_id': ObjectID(req.body.cs)})
		.then((result) => {
			let camera = null;
			if (result == null) {
				camera = [{name: "Camera 1"}];
			} else {
				camera = result.cameras[req.body.camera];
			}
			const videoFile = req.files['video'][0];

			const filestream = fs.ReadStream(videoFile.path);
			const hash = crypto.createHash('sha256');
			filestream.on('data', function (data) {
				hash.update(data)
			});

			filestream.on('end', function () {
				const name = hash.digest('hex') + '.' + mime.extension(videoFile.mimetype);
				const destPath = 'videos/' + name;
				if (fs.existsSync(destPath)) {
                    fs.unlinkSync(videoFile.path)
                } else {
                	fs.renameSync(videoFile.path, destPath);
                }

				let videoList = [];
				if(req.files['timings'] == null) {
					let video = {
						name: camera.name,
						file: name,
						recipe: result.currentRecipe,
						start: 0
					};
					videoList.push(video);
				} else {
					const timingsFile = req.files['timings'][0];
					const timings = fs.readFileSync(timingsFile.path).toString();
					const lines = timings.split(/[\r\n]+/);
					let shots = [{name: ''}];
					if (camera.shots != null) {
						shots = camera.shots;
					}
					for (const line of lines) {
						const values = line.split(',');

						for (const shot of shots) {
							let video = {
								name: camera.name + ' ' + shot.name + ' @' + formatMillis(values[0]),
								file: name,
								recipe: result.currentRecipe,
								start: values[0],
								end: values[1]
							};
							if (shot.zoom != null) {
								video.zoom = shot.zoom;
								video.x = shot.x;
								video.y = shot.y;
							}

							videoList.push(video);
						}
					}

					fs.unlinkSync(req.files['timings'][0].path);
				}

				db.collection('videos').insertMany(videoList)
					.then((result) => {
						console.log(`Added ${result.ops.length} videos`);
						res.json(result);
					})
					.catch((err) => {
						console.log(err);
						res.status(500).send(err);
					});
			});
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});

});

app.get('/api/videos/recipe', (req, res) => {
	let query = {};
	if (req.query.cs != null) {
		query = {'_id': req.query.params};
	}
	db.collection('cookingStations').findOne(query)
		.then((result) => {
			db.collection('videos').find({recipe: result.currentRecipe}).toArray()
				.then((result) => {
					res.json(result);
				})
				.catch((err) => {
					console.log(err);
					res.status(500).send(err);
				});
		});
});

app.get('/api/videos/recipe/:id', (req, res) => {
	db.collection('videos').find({recipe: ObjectID(req.params.id)}).toArray()
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/api/recipes', (req, res) => {
	db.collection('recipes').find().toArray()
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.post('/api/recipes/:parent', (req, res) => {
	let recipe = {
		steps: [{}]
	};
	if (req.params.parent !== 'root') {
		recipe.parent = ObjectID(req.params.parent);
	}
	recipe.author = req.body.author;
	recipe.email = req.body.email;
	console.log(recipe);
	db.collection('recipes').insertOne(recipe)
		.then((result) => {
			const recipeResult = result.ops[0];
			let query = {};
			if (req.body.cs != null) {
				query._id = ObjectID(req.query.cs);
			}
			console.log(query);
			db.collection('cookingStations').findOneAndUpdate(query,
				{$set: {'currentRecipe': recipeResult._id}})
				.then((result) => {
					console.log(result);
					res.json(recipeResult);
				})
				.catch((err) => {
					console.log(err);
					res.status(500).send(err);
				});
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/api/recipes/:id', (req, res) => {
	db.collection('recipes').findOne({'_id': ObjectID(req.params.id)})
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.put('/api/recipes/:id', (req, res) => {
	let recipe = req.body;
	delete recipe._id;
	delete recipe.id;
	delete recipe.parent;
	db.collection('recipes').findOneAndUpdate({'_id': ObjectID(req.params.id)}, {$set: recipe})
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.get('/api/recipes/:id/children', (req, res) => {
	let parent = req.params.id;
	if (parent === 'root') {
		parent = null;
	} else {
		parent = ObjectID(parent)
	}
	db.collection('recipes').find({'parent': parent}).toArray()
		.then((result) => {
			res.json(result);
		})
		.catch((err) => {
			console.log(err);
			res.status(500).send(err);
		});
});

app.use(function (req, res, next) {
	next(createError(404));
});

MongoClient.connect('mongodb://mongo:27017', (err, client) => {
	if (err) return console.log(err);
	db = client.db('openrecipe');

	const server = http.createServer(app);

	/**
	 * Listen on provided port, on all network interfaces.
	 */

	server.listen(8080);
	server.on('error', (error) => {
		if (error.syscall !== 'listen') {
			throw error;
		}

		const bind = typeof port === 'string'
			? 'Pipe ' + port
			: 'Port ' + port;

		// handle specific listen errors with friendly messages
		switch (error.code) {
			case 'EACCES':
				console.error(bind + ' requires elevated privileges');
				process.exit(1);
				break;
			case 'EADDRINUSE':
				console.error(bind + ' is already in use');
				process.exit(1);
				break;
			default:
				throw error;
		}
	});
	server.on('listening', () => {
		const addr = server.address();
		const bind = typeof addr === 'string'
			? 'pipe ' + addr
			: 'port ' + addr.port;
		debug('Listening on ' + bind);
	});
});