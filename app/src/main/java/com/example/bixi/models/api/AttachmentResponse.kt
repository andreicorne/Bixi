package com.example.bixi.models.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttachmentResponse(
    val id: String,
    val tenantId: String,
    val name: String,
    val type: String,
    val description: String?,
    val fileUrl: String,
    val prevFileUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val entityType: String,
    val entityId: String,
    val signatures: String?, // Schimbat din Any? în String? pentru Parcelable
    val status: String
) : Parcelable