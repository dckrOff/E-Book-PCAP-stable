package uz.dckroff.pcap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uz.dckroff.pcap.data.model.GlossaryRelatedSections
import uz.dckroff.pcap.data.model.GlossaryTerm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с глоссарием
 */
interface GlossaryRepository {
    /**
     * Получить все термины глоссария
     */
    suspend fun getAllTerms(): List<GlossaryTerm>

    /**
     * Получить термины глоссария по категории
     */
    suspend fun getTermsByCategory(category: String): List<GlossaryTerm>

    /**
     * Поиск терминов по тексту
     */
    suspend fun searchTerms(query: String): List<GlossaryTerm>

    /**
     * Получить термин по ID
     */
    suspend fun getTermById(termId: String): GlossaryTerm?

    /**
     * Получить список всех категорий
     */
    suspend fun getAllCategories(): List<String>
}

/**
 * Реализация репозитория глоссария с использованием Firestore
 */
@Singleton
class GlossaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GlossaryRepository {

    override suspend fun getAllTerms(): List<GlossaryTerm> {
        return try {
            val snapshot = firestore.collection("glossary")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    GlossaryTerm(
                        id = doc.id,
                        term = doc.getString("term") ?: return@mapNotNull null,
                        definition = doc.getString("definition") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: "",
                        relatedTerms = (doc.get("relatedTerms") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        relatedSections = (doc.get("relatedSections") as? List<*>)?.mapNotNull { it as? GlossaryRelatedSections } ?: emptyList()
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing glossary term document")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching glossary terms from Firestore")
            emptyList()
        }
    }

    override suspend fun getTermsByCategory(category: String): List<GlossaryTerm> {
        return try {
            val snapshot = firestore.collection("glossary")
                .whereEqualTo("category", category)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    GlossaryTerm(
                        id = doc.id,
                        term = doc.getString("term") ?: return@mapNotNull null,
                        definition = doc.getString("definition") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: "",
                        relatedTerms = (doc.get("relatedTerms") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        relatedSections = (doc.get("relatedSections") as? List<*>)?.mapNotNull { it as? GlossaryRelatedSections } ?: emptyList()
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing glossary term document")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching glossary terms by category from Firestore")
            emptyList()
        }
    }

    override suspend fun searchTerms(query: String): List<GlossaryTerm> {
        val lowercaseQuery = query.lowercase().trim()

        // Для полнотекстового поиска возвращаем все термины и фильтруем на клиенте
        return try {
            val allTerms = getAllTerms()
            allTerms.filter { term ->
                term.term.lowercase().contains(lowercaseQuery) ||
                term.definition.lowercase().contains(lowercaseQuery)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching glossary terms")
            emptyList()
        }
    }

    override suspend fun getTermById(termId: String): GlossaryTerm? {
        return try {
            val doc = firestore.collection("glossary")
                .document(termId)
                .get()
                .await()

            if (!doc.exists()) {
                return null
            }

            GlossaryTerm(
                id = doc.id,
                term = doc.getString("term") ?: return null,
                definition = doc.getString("definition") ?: return null,
                category = doc.getString("category") ?: "",
                relatedTerms = (doc.get("relatedTerms") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                relatedSections = (doc.get("relatedSections") as? List<*>)?.mapNotNull { it as? GlossaryRelatedSections } ?: emptyList()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error fetching glossary term from Firestore")
            null
        }
    }

    override suspend fun getAllCategories(): List<String> {
        return try {
            val terms = getAllTerms()
            terms.mapNotNull { it.category }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching glossary categories")
            emptyList()
        }
    }
} 