package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PropertyEntity::class,
        ReelEntity::class,
        LeadEntity::class,
        SavedPropertyEntity::class,
        ReferralCodeEntity::class,
        ReferralEntity::class,
        CoinLedgerEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PropReelDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun reelDao(): ReelDao
    abstract fun leadDao(): LeadDao
    abstract fun savedPropertyDao(): SavedPropertyDao
    abstract fun referralDao(): ReferralDao
    abstract fun coinLedgerDao(): CoinLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: PropReelDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reels ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'video'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `referral_codes` (
                        `code` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`code`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_referral_codes_userId` ON `referral_codes` (`userId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `referrals` (
                        `id` TEXT NOT NULL,
                        `referrerId` TEXT NOT NULL,
                        `referredUserId` TEXT NOT NULL,
                        `referralCode` TEXT NOT NULL,
                        `attributionSource` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `qualifiedAt` INTEGER,
                        `rewardedAt` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_referrals_referredUserId` ON `referrals` (`referredUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_referrals_referrerId` ON `referrals` (`referrerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_referrals_referralCode` ON `referrals` (`referralCode`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `coin_ledger` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `referenceId` TEXT,
                        `idempotencyKey` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_coin_ledger_idempotencyKey` ON `coin_ledger` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_coin_ledger_userId` ON `coin_ledger` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_coin_ledger_referenceId` ON `coin_ledger` (`referenceId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM reels WHERE id IN ('reel_01_video', 'reel_03_video')")
                db.execSQL("UPDATE properties SET linkedReelId = NULL WHERE linkedReelId IN ('reel_01_video', 'reel_03_video')")
            }
        }

        fun getInstance(context: Context): PropReelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PropReelDatabase::class.java,
                    "propreel.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
