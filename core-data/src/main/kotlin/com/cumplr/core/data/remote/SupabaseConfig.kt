package com.cumplr.core.data.remote

import com.cumplr.core.data.BuildConfig

object SupabaseConfig {
    val url: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
}
