// routes/rest.js
import express from 'express'
const router = express.Router()

import { createRoomIfNotExists } from '../roomManager.js'

router.post('/create-room', async (req, res) => {
    const { roomId } = req.body

    if (!roomId) {
        return res.status(400).json({ error: 'roomId is required' })
    }

    try {
        await createRoomIfNotExists(roomId)
        res.status(200).json({ status: 'ok' })
    } catch (err) {
        console.error(err)
        res.status(500).json({ error: 'Could not create room' })
    }
})

export default router
