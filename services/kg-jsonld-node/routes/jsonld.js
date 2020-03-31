/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var express = require('express');
const jsonld = require('jsonld');
var router = express.Router();
router.use(express.json());

router.post('/', function (req, res) {
  jsonld.expand(req.body, function (expandErr, expanded) {
    if (expandErr == null) {
      jsonld.compact(expanded, {}, function (compactErr, compacted) {
        if (compactErr == null) {
          res.send(compacted);
        } else {
          res.status(400);
          res.send("Was not able to compact JSON-LD", compactErr);
        }
      });
    } else {
      res.status(400);
      res.send({"error": "Was not able to expand JSON-LD", "detail": expandErr});
    }
  });
});

router.get('/health', function (req, res) {
  var payload = {
    "status": "UP"
  };
  res.status(200);
  res.send(payload);
});


module.exports = router;
