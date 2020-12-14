package com.martianlab.data.sources.db_new

import com.martianlab.data.sources.db_new.converters.Converters
import com.martianlab.data.sources.db_new.entities.RecipeWithDependenciesEntity
import com.martianlab.data.sources.db_new.mapper.toEntity
import com.martianlab.recipes.data.sources.db_new.mapper.toEntity
import com.martianlab.recipes.data.sources.db_new.mapper.toModel
import com.martianlab.data.sources.db_new.mapper.toRecipe
import com.martianlab.data.sources.db_new.mapper.toRecipeIngredient
import com.martianlab.recipes.data.sources.db_new.DatabaseDriverFactory
import com.martianlab.recipes.domain.api.DbApi
import com.martianlab.recipes.entities.*
import com.martianlab.recipes.shared.db.cache.AppDatabase
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import commartianlabrecipesshareddbcache.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext


internal class DbImpl(driverFactory: DatabaseDriverFactory) : DbApi {

    private val driver = driverFactory.createDriver()
    private val db = AppDatabase(driver, recipeCommentEntityAdapter = RecipeCommentEntity.Adapter(photoURLsAdapter = Converters.listOfStringsAdapter))


    private val recipesDb = db.recipeEntityQueries
    private val categoryDb = db.categoryEntityQueries

    private fun RecipeEntity.toRecipeWithDependenciesEntity() : RecipeWithDependenciesEntity {

        val tags = recipesDb.getTagsByRecipe(id).executeAsList().toSet()
        val stages = recipesDb.getStagesByRecipe(id).executeAsList()
        val comments = recipesDb.getCommentsByRecipe(id).executeAsList()
        val ingredients = recipesDb.getIngredientsByRecipe(id).executeAsList()

        return RecipeWithDependenciesEntity( this, tags, stages, ingredients, comments)
    }

    private fun Query<RecipeEntity>.toRecipeList(): Flow<List<Recipe>> =
            asFlow().mapToList{ it.toRecipeWithDependenciesEntity().toRecipe() }

    private fun <From :Any, To>  Flow<Query<From>>.mapToList( context: CoroutineContext = Dispatchers.Default, entityMapper: (From) -> To ): Flow<List<To>> =
            mapToList(context).map { list -> list.map { entityMapper(it)} }

    override suspend fun getRecipes(tag: RecipeTag): Flow<List<Recipe>> =
        recipesDb.getRecipesByTagTitle(tag.title).toRecipeList()

    override suspend fun getRecipes(): Flow<List<Recipe>> =
        recipesDb.getRecipes().toRecipeList()


    override suspend fun getRecipeById(id: Long): Recipe =
        recipesDb
            .getById(id)
            .executeAsOne()
            .toRecipeWithDependenciesEntity()
            .toRecipe()


    override suspend fun insert(recipe: Recipe): Long {
        recipesDb.insertRecipe(recipe.toEntity())
        with(recipe){
            insertCommentList(comments)
            insertStageList(stages)
            insertIngredientList(ingredients)
            insertTagList(tags)
        }
        return 1
    }

    override suspend fun insert(recipeList: List<Recipe>) =
        recipeList.forEach { insert(it) }

    private fun insertCommentList(list: List<RecipeComment>) =
        list.forEach { recipesDb.insertComment(it.toEntity()) }

    private fun insertIngredientList(list: List<RecipeIngredient>) =
        list.forEach { recipesDb.insertIngredient(it.toEntity()) }

    private fun insertStageList(list: List<RecipeStage>) =
        list.forEach { recipesDb.insertStage(it.toEntity()) }

    private fun insertTagList(list: List<RecipeTag>) =
        list.forEach { recipesDb.insertTag(it.toEntity()) }


    override suspend fun loadCategories(): Flow<List<Category>> =
        categoryDb.getAll().asFlow().mapToList{  it.toModel() }

    override suspend fun getCategories(): List<Category> =
        categoryDb.getAll().executeAsList().map { it.toModel() }


    override suspend fun insertCategories(categoryList: List<Category>): List<Long> {
        categoryList.forEach { categoryDb.insert(it.toEntity()) }
        return emptyList()
    }

    override suspend fun searchIngredients(contains: String): List<RecipeIngredient> =
        recipesDb.searchIngredients(contains).executeAsList().map { it.toRecipeIngredient() }

    override suspend fun searchRecipes(contains: String): Flow<List<Recipe>> =
        recipesDb.searchRecipes(contains).toRecipeList()

    override suspend fun setFavorite(recipe: Recipe) {
        TODO("Not yet implemented")
    }

    override suspend fun removeFavorite(recipe: Recipe) {
        TODO("Not yet implemented")
    }

    override suspend fun getFavorites(): List<Recipe> {
        TODO("Not yet implemented")
    }

    override suspend fun getRecipesByIngredient(ingredient: RecipeIngredient): List<Recipe> {
        TODO("Not yet implemented")
    }
}