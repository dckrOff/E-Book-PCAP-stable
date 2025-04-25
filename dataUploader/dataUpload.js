const admin = require('firebase-admin');
const serviceAccount = require('./google-services.json'); 

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Пример добавления главы
async function addChapter() {
  const chapterRef = await db.collection('chapters').add({
    title: 'Введение в параллельные вычисления',
    description: 'Основные понятия и принципы параллельных вычислений',
    order: 1,
    progress: 0,
    number: 1,
    hasSubchapters: true
  });
  
  console.log('Добавлена глава с ID:', chapterRef.id);
  return chapterRef.id;
}

// Пример добавления раздела
async function addSection(chapterId) {
  const sectionRef = await db.collection('sections').add({
    title: 'История развития параллельных вычислений',
    chapterId: chapterId,
    order: 1,
    progress: 0,
    number: '1.1'
  });
  
  console.log('Добавлен раздел с ID:', sectionRef.id);
  return sectionRef.id;
}

// Пример добавления содержимого раздела
async function addSectionContent(sectionId) {
  await db.collection('section_content').add({
    sectionId: sectionId,
    type: 'text',
    content: '<p><strong>История развития параллельных вычислений</strong></p><p>Параллельные вычисления имеют долгую историю...</p>',
    order: 1
  });
  
  console.log('Добавлен контент для раздела');
}

// Выполнение функций
async function setupTestData() {
  const chapterId = await addChapter();
  const sectionId = await addSection(chapterId);
  await addSectionContent(sectionId);
}

setupTestData().catch(console.error);