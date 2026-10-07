package com.example.contris.data

import com.example.contris.data.mapper.toDomain
import com.example.contris.data.mapper.toEntityOrNull
import com.example.contris.data.remote.dto.CountryDto
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The schema is "reviewed weekly"; these tests pin the shapes we tolerate without crashing the sync. */
class CountryDtoLeniencyTest {

    private fun parse(json: String): CountryDto = Fixtures.json.decodeFromString(json)

    @Test
    fun `currencies and languages as arrays`() {
        val dto = parse(
            """{"uuid":"u","names":{"common":"X"},
               "currencies":[{"code":"EUR","name":"Euro","symbol":"€"}],
               "languages":[{"name":"German","native_name":"Deutsch","bcp47":"de","iso639_3":"deu"}]}""",
        )
        assertThat(dto.currencies.single().code).isEqualTo("EUR")
        assertThat(dto.languages.single().iso6393).isEqualTo("deu")
    }

    @Test
    fun `currencies and languages as objects keyed by code`() {
        val dto = parse(
            """{"uuid":"u","names":{"common":"X"},
               "currencies":{"EUR":{"name":"Euro","symbol":"€"},"CHF":{"name":"Swiss franc","symbol":"Fr."}},
               "languages":{"deu":{"name":"German","native_name":"Deutsch","bcp47":"de"}}}""",
        )
        assertThat(dto.currencies.map { it.code }).containsExactly("EUR", "CHF").inOrder()
        assertThat(dto.currencies.first().name).isEqualTo("Euro")
        assertThat(dto.languages.single().iso6393).isEqualTo("deu")
        assertThat(dto.languages.single().name).isEqualTo("German")

        val entity = dto.toEntityOrNull()!!
        assertThat(entity.currencies).contains("\"code\":\"EUR\"")
    }

    @Test
    fun `null or unexpected collection values become empty lists`() {
        val dto = parse("""{"uuid":"u","names":{"common":"X"},"currencies":null,"languages":"n/a"}""")
        assertThat(dto.currencies).isEmpty()
        assertThat(dto.languages).isEmpty()
    }

    @Test
    fun `capital attributes and swatches tolerate odd shapes`() {
        val dto = parse(
            """{"uuid":"u","names":{"common":"X"},
               "capitals":[{"name":"Cap","attributes":{"primary":"yes","judicial":true,"note":{"x":1}}}],
               "flag":{"colors":{"dominant":"#111111","swatches":{"vibrant":{"hex":"#222222"},"muted":null,"light_muted":"#333333"}}}}""",
        )
        val entity = dto.toEntityOrNull()!!
        assertThat(entity.capitalName).isEqualTo("Cap")
        assertThat(entity.flagVibrant).isEqualTo("#222222")
        assertThat(entity.flagMuted).isNull()
        val capitals = entity.toDomain().capitals
        assertThat(capitals.single().isPrimary).isFalse() // "yes" is not a boolean
        assertThat(capitals.single().roles).containsExactly("judicial")
    }

    @Test
    fun `premium leaders notice and unknown blocks are ignored`() {
        val dto = parse("""{"uuid":"u","names":{"common":"X"},"leaders":{"notice":"paid plans only"},"assets":{"coat_of_arms":"…"}}""")
        assertThat(dto.toEntityOrNull()).isNotNull()
    }
}
