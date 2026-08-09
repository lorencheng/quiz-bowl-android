/**
 * PowerMark Feedback Webhook — Google Apps Script
 *
 * Setup (one-time):
 *  1. Create a new Google Sheet (name it "PowerMark Feedback" or similar).
 *  2. In the sheet: Extensions → Apps Script.
 *  3. Delete the default code and paste this entire file.
 *  4. Save (Ctrl+S).
 *  5. Click "Deploy" → "New deployment" → type: Web App.
 *     - Description: PowerMark feedback receiver
 *     - Execute as: Me
 *     - Who has access: Anyone
 *  6. Click "Deploy", authorize the permissions when prompted.
 *  7. Copy the Web App URL (looks like https://script.google.com/macros/s/.../exec).
 *  8. Paste that URL into FeedbackRepository.ENDPOINT in the Android project.
 *
 * Sheet columns (auto-created on first submission):
 *   Timestamp | App Version | Android Version | Device | Feedback | Screenshot URL
 *
 * Screenshots are saved to a "PowerMark Feedback Screenshots" folder in your Drive.
 */

function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);

    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();

    // Add header row if sheet is empty
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(['Timestamp', 'App Version', 'Android Version', 'Device', 'Feedback', 'Screenshot']);
      sheet.getRange(1, 1, 1, 6).setFontWeight('bold');
    }

    let screenshotUrl = '';
    if (data.screenshot) {
      const folder = getOrCreateFolder('PowerMark Feedback Screenshots');
      const bytes = Utilities.base64Decode(data.screenshot);
      const blob = Utilities.newBlob(bytes, 'image/jpeg', 'screenshot_' + Date.now() + '.jpg');
      const file = folder.createFile(blob);
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      screenshotUrl = file.getUrl();
    }

    sheet.appendRow([
      new Date(data.timestamp || Date.now()),
      data.appVersion || '',
      data.androidVersion || '',
      data.deviceModel || '',
      data.feedback || '',
      screenshotUrl,
    ]);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function getOrCreateFolder(name) {
  const folders = DriveApp.getFoldersByName(name);
  return folders.hasNext() ? folders.next() : DriveApp.createFolder(name);
}
