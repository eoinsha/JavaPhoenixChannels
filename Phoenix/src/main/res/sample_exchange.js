var exchanges = [
   {
      "topic" : "rooms:lobby",
      "event" : "phx_join",
      "payload" :
         {
         },
      "ref" : "1"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
            "status" : "ok",
            "response" :
               {
               },
            "ref" : "1"
         },
      "event" : "phx_reply"
   },
   {
      "topic" : "rooms:lobby",
      "event" : "new_msg",
      "payload" :
         {
            "body" : "Eoin"
         },
      "ref" : "2"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
            "status" : "ok",
            "response" :
               {
                  "updated_at" : "2015-04-24T05:26:13Z",
                  "room_id" : "lobby",
                  "inserted_at" : "2015-04-24T05:26:13Z",
                  "id" : 6,
                  "body" : "Eoin",
                  "__meta__" :
                     {
                        "state" : "loaded",
                        "source" : "messages"
                     }
               },
            "ref" : "2"
         },
      "event" : "phx_reply"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "rooms:lobby",
      "event" : "new_msg",
      "payload" :
         {
            "body" : "Shanaghy"
         },
      "ref" : "3"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
            "status" : "ok",
            "response" :
               {
                  "updated_at" : "2015-04-24T05:26:16Z",
                  "room_id" : "lobby",
                  "inserted_at" : "2015-04-24T05:26:16Z",
                  "id" : 7,
                  "body" : "Shanaghy",
                  "__meta__" :
                     {
                        "state" : "loaded",
                        "source" : "messages"
                     }
               },
            "ref" : "3"
         },
      "event" : "phx_reply"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "rooms:lobby",
      "ref" : null,
      "payload" :
         {
         },
      "event" : "ping"
   },
   {
      "topic" : "phoenix",
      "event" : "heartbeat"
   }
];