// Builds the FCM v1 message. HymnsFirebaseMessagingService reads data keys title / message / target_screen.
const ticket = $('Parse ticket').first().json;
const tokenRows = $input.first().json;
const rows = Array.isArray(tokenRows) ? tokenRows : [tokenRows];
const token = rows.find((row) => row && row.fcm_token)?.fcm_token;

if (!token) return []; // guest or no token stored: the Jira comment still reaches them in the app

return [{
  json: {
    message: {
      token,
      data: {
        title: `${ticket.songType} ${ticket.songNumber} lyrics`,
        message: 'We checked your report: the lyrics are correct and up to date. Open the app to read our reply.',
        target_screen: 'tickets',
      },
    },
  },
}];
