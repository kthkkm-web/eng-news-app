package app.myroutine.auth

import app.myroutine.domain.model.User
import app.myroutine.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 항상 로컬 사용자 1명을 반환한다. 서버 로그인을 붙일 때 이 인터페이스의 다른 구현을
 * AuthModule 에서 바인딩하고, 로컬 사용자 데이터를 서버 userId 로 이관하면 된다.
 */
@Singleton
class LocalAuthRepository @Inject constructor() : AuthRepository {
    private val user = MutableStateFlow<User?>(LOCAL_USER)
    override val currentUser: Flow<User?> = user
    override fun currentUserId(): String = LOCAL_USER.id
    override suspend fun signIn(): Result<User> = Result.failure(UnsupportedOperationException("로그인 준비 중"))
    override suspend fun signOut() = Unit

    companion object {
        val LOCAL_USER = User(id = "local-user", displayName = "로컬 사용자", isLocal = true)
    }
}
